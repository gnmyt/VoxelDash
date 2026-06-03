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
        '/api-reference/maven-package',
      ],
      failOnError: false,
    },
  },
});