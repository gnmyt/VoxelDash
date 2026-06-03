export default defineNitroPlugin((nitroApp) => {
  const apiVersion = useRuntimeConfig().apiVersion as string;

  nitroApp.hooks.hook('content:file:beforeParse', (file: { _id: string; body: string }) => {
    if (file._id.endsWith('.md') && file.body.includes('{{API_VERSION}}')) {
      file.body = file.body.replaceAll('{{API_VERSION}}', apiVersion);
    }
  });
});
