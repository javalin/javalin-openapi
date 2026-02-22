import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'Javalin OpenAPI',
  description: 'Compile-time OpenAPI integration for Javalin',
  base: '/javalin-openapi/',
  appearance: 'dark',
  themeConfig: {
    siteTitle: 'Javalin OpenAPI',
    nav: [
      { text: 'Documentation', link: '/introduction/setup' },
      {
        text: 'Community',
        items: [
          { text: 'GitHub Issues', link: 'https://github.com/javalin/javalin-openapi/issues' },
          { text: 'Javalin', link: 'https://javalin.io' },
        ],
      },
    ],
    sidebar: [
      {
        text: 'Introduction',
        items: [
          { text: 'Setup with Javalin', link: '/introduction/setup' },
          { text: 'Setup without Javalin', link: '/introduction/json-schema-setup' },
          { text: 'Javalin Swagger UI', link: '/introduction/swagger' },
          { text: 'Javalin ReDoc', link: '/introduction/redoc' },
        ],
      },
      {
        text: 'OpenAPI',
        collapsed: false,
        items: [
          { text: 'Getting Started', link: '/openapi/getting-started' },
          { text: 'Parameters', link: '/openapi/parameters' },
          { text: 'Request Body', link: '/openapi/request-body' },
          { text: 'Responses', link: '/openapi/responses' },
          { text: 'Schema Generation', link: '/openapi/schemas' },
          { text: 'Naming Strategies', link: '/openapi/naming' },
          { text: 'Enums', link: '/openapi/enums' },
          { text: 'Examples', link: '/openapi/examples' },
          { text: 'Validation', link: '/openapi/validation' },
        ],
      },
      {
        text: 'JSON Schema',
        collapsed: false,
        items: [
          { text: 'Getting Started', link: '/json-schema/getting-started' },
          { text: 'Type Composition', link: '/json-schema/composition' },
          { text: 'Custom Properties', link: '/json-schema/custom-properties' },
        ],
      },
      {
        text: 'Advanced',
        collapsed: false,
        items: [
          { text: 'Compile-time Configuration', link: '/advanced/configuration' },
          { text: 'Runtime Builder DSL', link: '/advanced/runtime-builder' },
        ],
      },
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/javalin/javalin-openapi' },
    ],
    outline: {
      level: [2, 3],
    },
    search: {
      provider: 'local',
    },
    footer: {
      copyright: 'Created by <a href="https://github.com/dzikoysk">dzikoysk</a> ❤️ · Part of the Javalin ecosystem',
    },
    editLink: {
      pattern: 'https://github.com/javalin/javalin-openapi/edit/main/docs/:path',
      text: 'Edit this page on GitHub',
    },
  },
})
