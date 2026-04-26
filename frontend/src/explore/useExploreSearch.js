import { ref } from 'vue';

export function useExploreSearch({ requestFn, apiPath, onError } = {}) {
  const keyword = ref('');
  const searchResults = ref([]);

  async function runSearch(value = keyword.value) {
    const nextKeyword = String(value || '').trim();
    keyword.value = nextKeyword;
    try {
      searchResults.value = await requestFn(apiPath, {
        params: { keyword: nextKeyword },
      });
      return searchResults.value;
    } catch (error) {
      searchResults.value = [];
      if (typeof onError === 'function') {
        onError(error);
      }
      return [];
    }
  }

  return {
    keyword,
    searchResults,
    runSearch,
  };
}
