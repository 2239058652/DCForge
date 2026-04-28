/** @type {import('tailwindcss').Config} */
export default {
    corePlugins: {
        borderWidth: true,
        borderLeftWidth: true
    },
    content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
    theme: {
        extend: {
            height: {
                15: '3.75rem' // 60px
            },
            colors: {},
            animation: {
                ripple: 'ripple 0.6s linear'
            },
            keyframes: {
                ripple: {
                    '0%': { transform: 'translate(-50%, -50%) scale(0)', opacity: 0.5 },
                    '100%': { transform: 'translate(-50%, -50%) scale(4)', opacity: 0 }
                }
            }
        }
    },
    plugins: []
}
