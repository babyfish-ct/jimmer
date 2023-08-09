module.exports = {
  presets: [require.resolve('@docusaurus/core/lib/babel/preset')],
  plugins: [
    [
        'import',
        { libraryName: '@mui/icons-material', style: true },
        '@mui/icons-material',
    ],
    [
        'import',
        { libraryName: '@mui/material', style: true },
        '@mui/material',
    ]
  ]
};
