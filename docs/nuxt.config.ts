// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  devtools: { enabled: true },
  extends: ['shadcn-docs-nuxt'],
  compatibilityDate: '2024-07-06',

  site: {
    url: 'https://voxeldash.dev',
  },

  ogImage: {
    enabled: false,
  },

  vite: {
    resolve: {
      alias: {
        '@unhead/vue/client': '@unhead/vue',
      },
    },
  },

  nitro: {
    prerender: {
      crawlLinks: true,
      ignore: ['/openapi.json'],
      routes: [
        '/',
        '/getting-started/introduction',
        '/getting-started/reverse-proxy',
        '/features/overview',
        '/features/players',
        '/features/file_manager',
        '/features/console',
        '/features/worlds',
        '/features/plugins',
        '/features/backups',
        '/features/schedules',
        '/features/configuration',
        '/api-reference/explorer',
      ],
      failOnError: false,
    },
  },
});