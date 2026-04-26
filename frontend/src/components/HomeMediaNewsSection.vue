<template>
  <div class="media-news-shell reveal-up">
    <header class="media-news-intro">
      <span class="section-kicker">导览介绍</span>
      <p>
        湿地被誉为 “地球之肾”，是生态文明建设的重要载体。习近平总书记多次强调，要推进湿地保护高质量发展，为子孙后代留下大美湿地。2026
        世界湿地日之际，我们深入贯彻落实总书记重要讲话精神，以湿地旅游信息管理系统为实践载体，统筹生态保护、智慧管理与文旅服务，以科技赋能湿地守护，践行人与自然和谐共生的现代化理念。
      </p>
    </header>

    <div class="media-news-layout">
      <article class="media-card">
        <div class="media-card__video-frame">
          <video
            class="media-card__video"
            :src="videoSrc"
            controls
            playsinline
            preload="metadata"
          ></video>
        </div>
      </article>

      <aside class="news-panel">
        <div class="news-card__header">
          <span class="section-kicker">新闻链接</span>
          <h2>湿地资讯</h2>
        </div>

        <div class="news-card__list">
          <a
            v-for="item in newsLinks"
            :key="item.url"
            class="news-item"
            :href="item.url"
            target="_blank"
            rel="noreferrer noopener"
          >
            <h3>
              <span class="news-item__title">{{ item.title }}</span>
              <span class="news-item__arrow" aria-hidden="true">‹</span>
            </h3>
            <div class="news-item__meta">
              <span>{{ item.source }}</span>
              <span>{{ item.date }}</span>
            </div>
          </a>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  videoSrc: {
    type: String,
    required: true
  },
  newsLinks: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.media-news-shell {
  width: min(1700px, calc(100% - 20px));
  margin: 0 auto;
  min-height: calc(100svh - 36px);
  display: grid;
  grid-template-rows: auto 1fr;
  align-content: center;
  gap: clamp(34px, 4vw, 56px);
  padding:
    clamp(118px, 10vw, 148px)
    clamp(28px, 4.8vw, 84px)
    clamp(52px, 5vw, 76px);
  color: var(--text-main);
  box-sizing: border-box;
}

.media-news-intro {
  display: grid;
  justify-items: center;
  gap: 12px;
  max-width: min(1180px, 100%);
  margin: 0 auto;
}

.media-news-intro p {
  margin: 0;
  color: var(--text-main);
  font-size: clamp(1.08rem, 1.22vw, 1.28rem);
  line-height: 1.9;
  letter-spacing: 0.01em;
  max-width: 68ch;
  text-indent: 2em;
  text-align: left;
  text-wrap: pretty;
}

.media-news-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(360px, 0.92fr);
  gap: clamp(24px, 2.4vw, 34px);
  width: 100%;
  align-items: start;
  align-self: stretch;
}

.media-card {
  min-width: 0;
  display: flex;
}

.news-card__header {
  display: grid;
  gap: 8px;
}

.news-card__header h2 {
  margin: 0;
  font-size: clamp(1.28rem, 1.55vw, 1.64rem);
  line-height: 1.22;
  letter-spacing: -0.01em;
}

.media-card__video-frame {
  border-radius: 0;
  overflow: hidden;
  background: transparent;
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.2);
  width: 100%;
  align-self: start;
}

.media-card__video {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  min-height: clamp(300px, 34vw, 430px);
  max-height: clamp(300px, 34vw, 430px);
  object-fit: cover;
  background: #000;
}

.news-panel {
  min-width: 0;
  padding-top: 6px;
  display: grid;
  align-content: start;
}

.news-card__list {
  display: grid;
  gap: 0;
  align-content: start;
  margin-top: 18px;
}

.news-item {
  display: grid;
  gap: 10px;
  padding: 12px 0;
  text-decoration: none;
  color: inherit;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  transition: transform 0.28s ease, border-color 0.28s ease;
}

.news-item:hover {
  transform: translateX(4px);
  border-color: rgba(191, 232, 217, 0.34);
}

.news-item__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--text-muted);
  font-size: 0.8rem;
  line-height: 1.5;
}

.news-item h3 {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin: 0;
  font-size: clamp(1.02rem, 1.18vw, 1.26rem);
  font-weight: 600;
  line-height: 1.58;
}

.news-item__title {
  flex: 1;
  min-width: 0;
  text-decoration: underline;
  text-decoration-color: transparent;
  text-decoration-thickness: 1px;
  text-underline-offset: 0.22em;
  transition: text-decoration-color 0.28s ease;
}

.news-item:hover .news-item__title {
  text-decoration-color: currentColor;
}

.news-item__arrow {
  flex: none;
  color: var(--text-muted);
  font-size: 1.1rem;
  line-height: 1.2;
}

@media (max-width: 1080px) {
  .media-news-layout {
    grid-template-columns: 1fr;
  }

  .media-card__video {
    min-height: 360px;
    max-height: none;
  }

  .news-panel {
    padding-top: 0;
  }
}

@media (max-width: 720px) {
  .media-news-shell {
    min-height: auto;
    padding: 104px 16px 56px;
    min-height: auto;
    gap: 28px;
  }

  .media-news-intro p {
    font-size: 1rem;
    line-height: 1.82;
    max-width: none;
  }

  .media-card__video {
    min-height: 240px;
    max-height: none;
  }

  .news-card__header h2 {
    font-size: 1.22rem;
  }

  .news-item__meta {
    align-items: flex-start;
  }

  .news-item h3 {
    font-size: 1rem;
  }
}
</style>
