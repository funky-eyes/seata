import type { useConsoleI18n } from '@/i18n';

export type NavigationItem = {
  key: string;
  path: string;
  label: string;
};

type Messages = ReturnType<typeof useConsoleI18n>['messages'];

export function navigationItems(messages: Messages): NavigationItem[] {
  return [
    {
      key: '/transaction/list',
      path: '/transaction/list',
      label: messages.nav.transactions
    },
    {
      key: '/transaction-group',
      path: '/transaction-group',
      label: messages.nav.txGroup
    },
    {
      key: '/globallock/list',
      path: '/globallock/list',
      label: messages.nav.locks
    },
    {
      key: '/cluster/list',
      path: '/cluster/list',
      label: messages.nav.cluster
    },
    {
      key: '/sagastatemachinedesigner',
      path: '/sagastatemachinedesigner',
      label: messages.nav.saga
    }
  ];
}