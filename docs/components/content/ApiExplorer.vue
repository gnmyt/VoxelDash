<script setup lang="ts">
import { ApiReference } from '@scalar/api-reference';
import '@scalar/api-reference/style.css';

const colorMode = useColorMode();

const configuration = computed(() => ({
  url: '/openapi.json',
  darkMode: colorMode.value === 'dark',
  hideDarkModeToggle: true,
  hideClientButton: false,
  agent: { disabled: true },
  metaData: { title: 'VoxelDash API Reference' },
}));

const explorerRef = ref<HTMLElement | null>(null);
const MIN_HEIGHT = 480;
const BOTTOM_GAP = 24;

function updateHeight() {
  const el = explorerRef.value;
  if (!el) return;
  const top = el.getBoundingClientRect().top;
  const px = `${Math.max(MIN_HEIGHT, Math.round(window.innerHeight - top - BOTTOM_GAP))}px`;
  el.style.height = px;
  el.style.setProperty('--full-height', px);
}

onMounted(() => {
  nextTick(updateHeight);
  setTimeout(updateHeight, 400);
  window.addEventListener('resize', updateHeight);
});
onBeforeUnmount(() => window.removeEventListener('resize', updateHeight));
</script>

<template>
  <ClientOnly>
    <div ref="explorerRef" class="api-explorer">
      <ApiReference :configuration="configuration" />
    </div>
    <template #fallback>
      <div class="api-explorer-loading">
        Loading API reference…
      </div>
    </template>
  </ClientOnly>
</template>

<style scoped>
.api-explorer {
  margin-top: 1rem;
  height: calc(100vh - 13rem);
  min-height: 600px;
  overflow: auto;
  overscroll-behavior: contain;
  border: 1px solid hsl(var(--border));
  border-radius: 0.5rem;
}
.api-explorer :deep(.references-layout) {
  min-height: 100%;
}
.api-explorer-loading {
  padding: 4rem 1rem;
  text-align: center;
  color: hsl(var(--muted-foreground));
}
</style>
