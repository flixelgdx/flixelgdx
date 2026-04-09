/**
 * Dev server: do not historyApiFallback Dokka HTML under /dokka-html/ to the SPA shell.
 */
export default function docusaurusApiStaticPlugin(context) {
  const base = context.siteConfig.baseUrl.replace(/\/$/, '');
  const escaped = base.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const from =
    base === ''
      ? /^\/dokka-html\/.*$/
      : new RegExp(`^${escaped}/dokka-html/.*$`);

  return {
    name: 'docusaurus-api-static-dev',
    configureWebpack(_config, isServer) {
      if (isServer) {
        return {};
      }
      return {
        devServer: {
          historyApiFallback: {
            rewrites: [{from, to: (ctx) => ctx.parsedUrl.pathname}],
          },
        },
      };
    },
  };
}
