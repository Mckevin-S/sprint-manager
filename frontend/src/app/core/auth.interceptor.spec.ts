import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  const auth = jasmine.createSpyObj<AuthService>('AuthService', ['token', 'logout']);

  beforeEach(() => {
    auth.token.and.returnValue('jwt');
    auth.logout.calls.reset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  it("ajoute le jeton aux appels de l'API", () => {
    http.get('/api/projects').subscribe();
    expect(backend.expectOne('/api/projects').request.headers.get('Authorization')).toBe('Bearer jwt');
  });

  it("n'envoie pas le jeton à la connexion ni hors de l'API", () => {
    http.post('/api/auth/login', {}).subscribe();
    http.get('/assets/config.json').subscribe();
    expect(backend.expectOne('/api/auth/login').request.headers.has('Authorization')).toBeFalse();
    expect(backend.expectOne('/assets/config.json').request.headers.has('Authorization')).toBeFalse();
  });

  it('déconnecte l\'utilisateur si le jeton est refusé', () => {
    http.get('/api/projects').subscribe({ error: () => undefined });
    backend.expectOne('/api/projects').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(auth.logout).toHaveBeenCalled();
  });
});
