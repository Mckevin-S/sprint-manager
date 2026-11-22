import { ChangeDetectionStrategy, Component } from '@angular/core';
import { LogoComponent } from '../../shared/logo.component';

/** Écran partagé connexion / inscription : présentation du produit à gauche, formulaire à droite. */
@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [LogoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="layout">
      <aside class="brand">
        <app-logo />
        <div class="pitch">
          <h1>Le rythme de votre équipe, <em>sprint après sprint.</em></h1>
          <p>Backlog priorisé, tableau Kanban avec limites de travail en cours, burndown et vélocité calculés
            à partir de l'historique réel de l'équipe.</p>
        </div>
        <div class="mini-board" aria-hidden="true">
          @for (column of columns; track column.label) {
            <div class="mini-col">
              <span class="mini-title">{{ column.label }}</span>
              @for (card of column.cards; track $index) {
                <span class="mini-card" [style.width.%]="card"></span>
              }
            </div>
          }
        </div>
        <span class="credit">Projet de démonstration · Spring Boot 3 &amp; Angular 17</span>
      </aside>
      <main class="form-side">
        <div class="form-wrap"><ng-content /></div>
      </main>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: minmax(380px, 46%) 1fr; min-height: 100vh; }
    .brand { background: var(--sidebar); color: #fff; padding: 40px 48px; display: flex; flex-direction: column; gap: 40px;
      position: relative; overflow: hidden; }
    .brand::after { content: ''; position: absolute; width: 520px; height: 520px; border-radius: 50%; right: -220px; bottom: -260px;
      background: radial-gradient(circle, rgba(232, 89, 12, 0.35), transparent 65%); }
    .pitch h1 { font-size: 36px; line-height: 1.15; margin: 0 0 16px; color: #fff; font-weight: 700; max-width: 460px; }
    .pitch em { font-style: normal; color: #ff9f66; }
    .pitch p { color: #b9bdd3; font-size: 15px; max-width: 440px; margin: 0; }
    .mini-board { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; max-width: 460px; position: relative; z-index: 1; }
    .mini-col { background: rgba(255, 255, 255, 0.05); border-radius: 10px; padding: 10px; display: flex; flex-direction: column; gap: 6px; }
    .mini-title { font-size: 10px; text-transform: uppercase; letter-spacing: 0.06em; color: #8f93b0; margin-bottom: 2px; }
    .mini-card { height: 22px; border-radius: 6px; background: rgba(255, 255, 255, 0.14); }
    .mini-col:last-child .mini-card { background: rgba(79, 209, 154, 0.35); }
    .credit { margin-top: auto; font-size: 12px; color: #7d819c; position: relative; z-index: 1; }
    .form-side { display: flex; align-items: center; justify-content: center; padding: 40px 24px; background: var(--bg); }
    .form-wrap { width: 100%; max-width: 400px; }
    @media (max-width: 900px) {
      .layout { grid-template-columns: 1fr; }
      .brand { padding: 24px; gap: 20px; }
      .pitch h1 { font-size: 26px; }
      .mini-board, .credit { display: none; }
    }
  `,
})
export class AuthLayoutComponent {
  readonly columns = [
    { label: 'À faire', cards: [90, 70, 80] },
    { label: 'En cours', cards: [85, 60] },
    { label: 'Revue', cards: [75] },
    { label: 'Terminé', cards: [80, 95, 65, 70] },
  ];
}
