/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: 'rgb(var(--wk-primary-rgb) / <alpha-value>)',
        'background-light': 'rgb(var(--wk-bg-page-rgb) / <alpha-value>)',
        'background-dark': 'rgb(var(--wk-bg-page-rgb) / <alpha-value>)',
      },
      fontFamily: {
        sans: [
          'system-ui',
          '-apple-system',
          'BlinkMacSystemFont',
          '"Segoe UI"',
          '"PingFang SC"',
          '"Hiragino Sans GB"',
          '"Noto Sans CJK SC"',
          '"Noto Sans SC"',
          '"Microsoft YaHei"',
          'Arial',
          'sans-serif',
        ],
        display: [
          'system-ui',
          '-apple-system',
          'BlinkMacSystemFont',
          '"Segoe UI"',
          '"PingFang SC"',
          '"Hiragino Sans GB"',
          '"Noto Sans CJK SC"',
          '"Noto Sans SC"',
          '"Microsoft YaHei"',
          'Arial',
          'sans-serif',
        ],
      },
    },
  },
  plugins: [],
}
