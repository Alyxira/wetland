<template>
  <main class="travel-map-host">
    <div class="travel-map-shell">
      <section class="travel-map-stage">
        <iframe
          class="travel-map-frame"
          :src="frameSrc"
          title="Travel Map"
          loading="eager"
          referrerpolicy="no-referrer"
        ></iframe>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const frameSrc = computed(() => {
  const params = new URLSearchParams()
  const spot = Array.isArray(route.query?.spot) ? route.query.spot[0] : route.query?.spot
  const normalizedSpot = String(spot || '').trim()
  if (normalizedSpot) {
    params.set('spot', normalizedSpot)
  }
  const queryString = params.toString()
  return queryString ? `/travel-map/?${queryString}` : '/travel-map/'
})
</script>

<style scoped>
.travel-map-host {
  width: 100%;
  min-height: 100vh;
  background: transparent;
}

.travel-map-shell {
  min-height: 100vh;
  position: relative;
  padding: 0;
}

.travel-map-stage {
  width: 100%;
  height: 100vh;
  border: 0;
  background: transparent;
  box-shadow: none;
  overflow: hidden;
}

.travel-map-frame {
  width: 100%;
  height: 100%;
  border: 0;
  display: block;
}

</style>
