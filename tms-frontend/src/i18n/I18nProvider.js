import React, { createContext, useContext, useState, useCallback } from 'react';
import { IntlProvider } from 'react-intl';
import en from './messages/en.json';
import hi from './messages/hi.json';

const messages = { en, hi };
const I18nContext = createContext(null);

export function I18nProvider({ children }) {
  const [locale, setLocale] = useState(() => localStorage.getItem('locale') || 'en');

  const changeLocale = useCallback((newLocale) => {
    setLocale(newLocale);
    localStorage.setItem('locale', newLocale);
  }, []);

  return (
    <I18nContext.Provider value={{ locale, setLocale: changeLocale }}>
      <IntlProvider locale={locale} messages={messages[locale] || messages.en} defaultLocale="en">
        {children}
      </IntlProvider>
    </I18nContext.Provider>
  );
}

export function useI18n() {
  const context = useContext(I18nContext);
  if (!context) throw new Error('useI18n must be used within I18nProvider');
  return context;
}

