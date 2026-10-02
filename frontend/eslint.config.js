import js from '@eslint/js'
import reactHooks from 'eslint-plugin-react-hooks'
import globals from 'globals'
import tseslint from 'typescript-eslint'

export default tseslint.config(
  { ignores: ['dist', 'coverage', 'src/api/schema.gen.ts'] },
  {
    files: ['**/*.{ts,tsx}'],
    extends: [js.configs.recommended, ...tseslint.configs.recommended, reactHooks.configs.flat.recommended],
    languageOptions: { globals: globals.browser },
    rules: {
      // Same rule as the backend's "every variable final": no reassignable variables at all.
      'prefer-const': 'error',
      'no-var': 'error',
      'no-restricted-syntax': [
        'error',
        { selector: "VariableDeclaration[kind='let']", message: 'Use const; variables are never reassigned.' },
      ],
      'no-param-reassign': ['error', { props: false }],
    },
  },
)
