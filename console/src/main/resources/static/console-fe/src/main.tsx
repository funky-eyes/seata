import '@ant-design/v5-patch-for-react-19';

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import App from './app';

const rootElement = document.getElementById('root');

if (!rootElement) {
  throw new Error('Console root element is missing.');
}

createRoot(rootElement).render(
  <StrictMode>
    <App />
  </StrictMode>
);