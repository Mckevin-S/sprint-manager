import { expect, test } from '@playwright/test';
import { loginAs, openDemoProject } from './helpers';

test.describe('Parcours développeur', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'mckevin');
    await openDemoProject(page);
  });

  test('un développeur termine la story qu’il a fait relire', async ({ page }) => {
    await page.getByTestId('nav-tableau').click();
    await page.getByText("Suivi en temps réel d'un dossier sinistre").click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByText('En revue', { exact: true })).toBeVisible();

    await dialog.getByTestId('move-DONE').click();

    await expect(dialog.getByText('Terminé', { exact: true })).toBeVisible();
    await dialog.getByRole('button', { name: 'Fermer' }).click();
    await expect(page.getByTestId('column-DONE').getByText("Suivi en temps réel d'un dossier sinistre")).toBeVisible();
  });

  test('un développeur crée une tâche mais pas de story', async ({ page }) => {
    await page.getByTestId('create-issue').click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.locator('mat-button-toggle', { hasText: 'Story' }).locator('button')).toBeDisabled();
    await expect(dialog.getByText('Les user stories sont rédigées par le Product Owner.')).toBeVisible();

    await dialog.getByTestId('issue-title').fill('Préparer la démo de fin de sprint');
    await dialog.getByTestId('issue-submit').click();

    await expect(page.getByText(/ORN-\d+ créé\./)).toBeVisible();
  });

  test('un développeur ne voit pas les actions de planification', async ({ page }) => {
    await page.getByTestId('nav-backlog').click();
    await expect(page.getByRole('heading', { name: 'Backlog produit' })).toBeVisible();
    await expect(page.getByTestId('create-sprint')).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Démarrer' })).toHaveCount(0);
  });
});

test('la déconnexion ramène à l’écran de connexion', async ({ page }) => {
  await loginAs(page, 'hugo');
  await page.getByRole('button', { name: 'Mon compte' }).click();
  await page.getByRole('menuitem', { name: 'Se déconnecter' }).click();
  await expect(page).toHaveURL(/\/connexion$/);
});
