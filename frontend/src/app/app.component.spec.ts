import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app.component';
import { ThemeService } from './core/theme.service';

describe('AppComponent', () => {
  beforeEach(() => localStorage.removeItem('sprint-manager.theme'));

  it('applique le thème enregistré au démarrage', () => {
    localStorage.setItem('sprint-manager.theme', 'dark');
    TestBed.configureTestingModule({ imports: [AppComponent], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    TestBed.flushEffects();

    expect(document.documentElement.classList.contains('dark-theme')).toBeTrue();
    TestBed.inject(ThemeService).toggle();
    TestBed.flushEffects();
    expect(document.documentElement.classList.contains('dark-theme')).toBeFalse();
  });
});
