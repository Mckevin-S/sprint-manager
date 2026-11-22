import { Page, expect } from '@playwright/test';

export type DemoRole = 'camille' | 'hugo' | 'mckevin';

/** Connexion avec un compte de démonstration via le raccourci de l'écran de connexion. */
export async function loginAs(page: Page, who: DemoRole): Promise<void> {
  await page.goto('/connexion');
  await page.getByTestId(`demo-${who}@demo.fr`).click();
  await expect(page).toHaveURL(/\/projets$/);
}

export async function openDemoProject(page: Page): Promise<void> {
  await page.getByTestId('project-ORN').click();
  await expect(page.getByRole('heading', { name: "Vue d'ensemble" })).toBeVisible();
}
