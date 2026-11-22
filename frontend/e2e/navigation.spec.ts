import { expect, test } from '@playwright/test';
import { loginAs, openDemoProject } from './helpers';

test.describe('Parcours Product Owner', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'camille');
    await openDemoProject(page);
  });

  test("la vue d'ensemble présente le sprint en cours", async ({ page }) => {
    await expect(page.getByText('Sprint 4').first()).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Burndown' })).toBeVisible();
    await expect(page.getByRole('heading', { name: "Charge de l'équipe" })).toBeVisible();
    await expect(page.locator('canvas')).toBeVisible();
  });

  test('le tableau affiche les quatre colonnes du workflow', async ({ page }) => {
    await page.getByTestId('nav-tableau').click();
    for (const status of ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE']) {
      await expect(page.getByTestId(`column-${status}`)).toBeVisible();
    }
    await expect(page.getByText("Suivi en temps réel d'un dossier sinistre")).toBeVisible();
  });

  test('le backlog montre le sprint planifié et les stories à estimer', async ({ page }) => {
    await page.getByTestId('nav-backlog').click();
    await expect(page.getByRole('heading', { name: 'Sprint 5' })).toBeVisible();
    await expect(page.getByText('story(s) à estimer avant planification')).toBeVisible();
  });

  test('les rapports affichent burndown et vélocité', async ({ page }) => {
    await page.getByTestId('nav-rapports').click();
    await expect(page.getByText('Vélocité moyenne', { exact: true })).toBeVisible();
    await expect(page.locator('canvas')).toHaveCount(2);
  });

  test('le thème sombre se mémorise', async ({ page }) => {
    await page.getByRole('button', { name: 'Thème sombre' }).click();
    await expect(page.locator('html')).toHaveClass(/dark-theme/);
    await page.reload();
    await expect(page.locator('html')).toHaveClass(/dark-theme/);
  });
});
