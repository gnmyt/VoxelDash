export default defineAppConfig({
  shadcnDocs: {
    site: {
      name: 'VoxelDash',
      description: 'VoxelDash (formerly MCDash) is a modern, open-source web dashboard for managing your Minecraft server — players, files, console, plugins, backups and more, from your browser.',
      ogImage: 'https://voxeldash.dev/img/screenshots/overview.png',
    },
    theme: {
      customizable: false,
      color: 'violet',
      radius: 0.75,
    },
    header: {
      title: 'VoxelDash',
      showTitle: true,
      darkModeToggle: true,
      logo: {
        light: '/img/favicon.png',
        dark: '/img/favicon.png',
      },
      links: [{
        icon: 'lucide:github',
        to: 'https://github.com/gnmyt/VoxelDash',
        target: '_blank',
      }],
    },
    aside: {
      useLevel: true,
      collapse: false,
    },
    main: {
      breadCrumb: true,
      showTitle: true,
    },
    footer: {
      credits: 'Copyright © 2025',
      links: [{
        icon: 'lucide:github',
        to: 'https://github.com/gnmyt/VoxelDash',
        target: '_blank',
      }],
    },
    toc: {
      enable: true,
      title: 'On This Page',
      links: [{
        title: 'Star on GitHub',
        icon: 'lucide:star',
        to: 'https://github.com/gnmyt/VoxelDash',
        target: '_blank',
      }, {
        title: 'Create Issues',
        icon: 'lucide:circle-dot',
        to: 'https://github.com/gnmyt/VoxelDash/issues',
        target: '_blank',
      }],
    },
    search: {
      enable: true,
      inAside: false,
    }
  }
});