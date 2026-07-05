// @ts-check
import { defineConfig } from 'astro/config';
import vue from '@astrojs/vue';

export default defineConfig({
  // used for canonical URLs and OG tags
  site: 'https://fri.sjomli.is',
  integrations: [vue()],
});
