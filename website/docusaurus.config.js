// @ts-check
import {themes as prismThemes} from 'prism-react-renderer';

import docusaurusApiStaticPlugin from './docusaurus-api-static-plugin.js';

/**
 * GitHub Pages project site lives at /flixelgdx/ — that is the default.
 *
 * If you open the `build` folder from a static server whose URL root *is* that folder
 * (or you use `npm run serve` / `npm run build:local`), override:
 *   DOCUSAURUS_BASE_URL=/ npm run build
 *
 * Do not open `build/index.html` via a long nested path unless baseUrl matches how the
 * server resolves `/…` paths; prefer `npm run serve` after `npm run build`.
 */
const baseUrlFromEnv = process.env.DOCUSAURUS_BASE_URL ?? '/flixelgdx/';
const baseUrl = baseUrlFromEnv.endsWith('/')
  ? baseUrlFromEnv
  : `${baseUrlFromEnv}/`;

const lightCodeTheme = prismThemes.github;
const darkCodeTheme = prismThemes.dracula;

const githubIconSvg = `${baseUrl}img/github-mark.svg`;

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'FlixelGDX',
  tagline: 'The most powerful game development framework for Java',
  url: 'https://stringdotjar.github.io',
  baseUrl,
  organizationName: 'stringdotjar',
  projectName: 'flixelgdx',
  trailingSlash: false,
  // /dokka-html/ is generated HTML (Dokka). The React API browser lives at /api.
  onBrokenLinks: 'ignore',
  staticDirectories: ['static'],
  plugins: [docusaurusApiStaticPlugin],

  presets: [
    [
      '@docusaurus/preset-classic',
      /** @type {import('@docusaurus/preset-classic').Options} */ ({
        docs: false,
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */ ({
      image: 'img/flixelgdx-logo.png',
      navbar: {
        title: ' ',
        logo: {
          alt: 'FlixelGDX',
          src: 'img/flixelgdx-logo.png',
        },
        items: [
          {to: '/', label: 'Home', position: 'left'},
          {to: '/getting-started', label: 'Getting Started', position: 'left'},
          {to: '/your-first-project', label: 'Your First Project', position: 'left'},
          {to: '/api', label: 'API', position: 'left'},
          {
            type: 'html',
            position: 'right',
            value: `<a class="navbar-github-link" href="https://github.com/stringdotjar/flixelgdx" target="_blank" rel="noopener noreferrer" aria-label="FlixelGDX on GitHub"><img class="navbar-github-icon" src="${githubIconSvg}" alt="" width="34" height="34" /></a>`,
          },
        ],
      },
      footer: {
        style: 'dark',
        copyright: `Copyright © ${new Date().getFullYear()} stringdotjar and contributors to FlixelGDX.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
        additionalLanguages: ['java', 'gradle', 'kotlin', 'groovy', 'bash'],
      },
      colorMode: {
        defaultMode: 'dark',
        respectPrefersColorScheme: true,
      },
    }),
};

export default config;
