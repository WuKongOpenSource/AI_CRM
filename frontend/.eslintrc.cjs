const unusedImportRules = {
  'no-unused-vars': 'off',
  '@typescript-eslint/no-unused-vars': 'off',
  'unused-imports/no-unused-imports': 'error',
  'unused-imports/no-unused-vars': [
    'warn',
    {
      vars: 'all',
      varsIgnorePattern: '^_',
      args: 'after-used',
      argsIgnorePattern: '^_'
    }
  ]
}

module.exports = {
  root: true,
  ignorePatterns: ['dist/', 'node_modules/', '*.tsbuildinfo'],
  overrides: [
    {
      files: ['**/*.{js,jsx,cjs,mjs,ts,tsx,cts,mts}'],
      parser: '@typescript-eslint/parser',
      parserOptions: {
        ecmaVersion: 'latest',
        sourceType: 'module'
      },
      plugins: ['@typescript-eslint', 'unused-imports'],
      rules: unusedImportRules
    },
    {
      files: ['**/*.vue'],
      parser: 'vue-eslint-parser',
      parserOptions: {
        parser: '@typescript-eslint/parser',
        ecmaVersion: 'latest',
        sourceType: 'module',
        extraFileExtensions: ['.vue']
      },
      plugins: ['@typescript-eslint', 'unused-imports', 'vue'],
      rules: {
        ...unusedImportRules,
        'vue/script-setup-uses-vars': 'error'
      }
    }
  ]
}
