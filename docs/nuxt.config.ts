import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

function readApiVersion(): string {
  try {
    const pom = resolve(dirname(fileURLToPath(import.meta.url)), '../modules/api/pom.xml');
    const xml = readFileSync(pom, 'utf8');
    return xml.match(/<artifactId>voxeldash-api<\/artifactId>\s*<version>([^<]+)<\/version>/)?.[1] ?? 'latest';
  } catch {
    return 'latest';
  }
}
const apiVersion = readApiVersion();

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  runtimeConfig: {
    apiVersion,
  },

  devtools: { enabled: true },
  extends: ['shadcn-docs-nuxt'],
  modules: ['@nuxtjs/sitemap', '@nuxtjs/robots'],
  compatibilityDate: '2024-07-06',

  site: {
    url: 'https://voxeldash.dev',
    name: 'VoxelDash',
    description: 'VoxelDash (formerly MCDash) is a modern, open-source web dashboard for managing your Minecraft server — monitor performance, manage players, edit files, install plugins and mods, schedule tasks and more, right from your browser. Works with Spigot, Paper, Fabric, Forge and vanilla.',
  },

  app: {
    head: {
      htmlAttrs: { lang: 'en' },
      meta: [
        { name: 'description', content: 'VoxelDash (formerly MCDash) is a modern, open-source web dashboard for managing your Minecraft server — monitor performance, manage players, edit files, install plugins and mods, schedule tasks and more, right from your browser. Works with Spigot, Paper, Fabric, Forge and vanilla.' },
        { name: 'keywords', content: 'VoxelDash, MCDash, Minecraft server dashboard, Minecraft web panel, Minecraft server manager, Minecraft control panel, Spigot, Paper, Fabric, Forge, vanilla, server management' },
        { name: 'author', content: 'GNM' },
        { name: 'theme-color', content: '#5e07b6' },
        { property: 'og:type', content: 'website' },
        { property: 'og:site_name', content: 'VoxelDash' },
        { property: 'og:image', content: 'https://voxeldash.dev/img/screenshots/overview.png' },
        { name: 'twitter:card', content: 'summary_large_image' },
        { name: 'twitter:image', content: 'https://voxeldash.dev/img/screenshots/overview.png' },
      ],
      script: [
        {
          type: 'application/ld+json',
          innerHTML: JSON.stringify({
            '@context': 'https://schema.org',
            '@type': 'SoftwareApplication',
            'name': 'VoxelDash',
            'alternateName': 'MCDash',
            'applicationCategory': 'DeveloperApplication',
            'operatingSystem': 'Cross-platform',
            'url': 'https://voxeldash.dev',
            'description': 'VoxelDash (formerly MCDash) is a modern, open-source web dashboard for managing your Minecraft server. Works with Spigot, Paper, Fabric, Forge and vanilla.',
            'offers': { '@type': 'Offer', 'price': '0', 'priceCurrency': 'USD' },
            'author': { '@type': 'Organization', 'name': 'GNM', 'url': 'https://gnm.dev' },
          }),
        },
      ],
    },
  },

  robots: {
    allow: '/',
    sitemap: 'https://voxeldash.dev/sitemap.xml',
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
      ignore: ['/openapi.json', '/one.sh', '/uninstall-one.sh'],
      routes: [
        '/',
        '/getting-started/introduction',
        '/getting-started/reverse-proxy',
        '/voxeldash-one/introduction',
        '/voxeldash-one/installation',
        '/features/overview',
        '/features/players',
        '/features/file_manager',
        '/features/console',
        '/features/motd',
        '/features/worlds',
        '/features/profiling',
        '/features/plugins',
        '/features/backups',
        '/features/schedules',
        '/features/configuration',
        '/features/gamerules',
        '/api-reference/explorer',
        '/api-reference/maven-package',
      ],
      failOnError: false,
    },
  },
});