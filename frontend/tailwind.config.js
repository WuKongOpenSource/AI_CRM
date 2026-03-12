/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#137fec',
        'background-light': '#f6f7f8',
        'background-dark': '#101922',
      },
      fontFamily: {
        sans: ['Inter', 'Noto Sans SC', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        display: ['Inter', 'Public Sans', 'Noto Sans SC', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
