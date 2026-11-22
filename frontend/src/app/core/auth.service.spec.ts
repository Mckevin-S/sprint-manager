import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;
  const response = {
    token: 'jwt',
    expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
    user: { id: 1, email: 'lea@test.fr', fullName: 'Léa Martin', initials: 'LM' },
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('conserve la session après connexion', () => {
    service.login('lea@test.fr', 'secret123').subscribe();
    http.expectOne('/api/auth/login').flush(response);

    expect(service.isAuthenticated()).toBeTrue();
    expect(service.token()).toBe('jwt');
    expect(service.user()?.initials).toBe('LM');
    expect(localStorage.getItem('sprint-manager.session')).toContain('jwt');
  });

  it('efface la session et redirige à la déconnexion', () => {
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    service.login('lea@test.fr', 'secret123').subscribe();
    http.expectOne('/api/auth/login').flush(response);

    service.logout();

    expect(service.isAuthenticated()).toBeFalse();
    expect(localStorage.getItem('sprint-manager.session')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/connexion']);
  });

  it("ignore une session enregistrée qui a expiré", () => {
    localStorage.setItem('sprint-manager.session', JSON.stringify({ ...response, expiresAt: '2000-01-01T00:00:00Z' }));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });

    expect(TestBed.inject(AuthService).isAuthenticated()).toBeFalse();
  });
});
