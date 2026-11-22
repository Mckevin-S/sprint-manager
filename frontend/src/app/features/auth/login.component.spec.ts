import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  const auth = jasmine.createSpyObj<AuthService>('AuthService', ['login']);

  function setup() {
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [provideRouter([]), provideNoopAnimations(), { provide: AuthService, useValue: auth }],
    });
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => auth.login.calls.reset());

  it("connecte l'utilisateur avec un compte de démonstration", () => {
    auth.login.and.returnValue(of({ token: 't', expiresAt: '', user: { id: 1, email: '', fullName: '', initials: '' } }));
    const fixture = setup();
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);

    fixture.componentInstance.useDemo(fixture.componentInstance.demoAccounts[1]);

    expect(auth.login).toHaveBeenCalledWith('hugo@demo.fr', 'demo1234');
    expect(router.navigate).toHaveBeenCalledWith(['/projets']);
  });

  it("affiche le message d'erreur de l'API", () => {
    auth.login.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 401, error: { detail: 'E-mail ou mot de passe incorrect.' },
    })));
    const fixture = setup();
    fixture.componentInstance.form.setValue({ email: 'lea@test.fr', password: 'mauvais' });

    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.componentInstance.error()).toBe('E-mail ou mot de passe incorrect.');
    expect((fixture.nativeElement as HTMLElement).querySelector('[role=alert]')?.textContent).toContain('incorrect');
  });

  it("n'appelle pas l'API si le formulaire est invalide", () => {
    const fixture = setup();
    fixture.componentInstance.submit();
    expect(auth.login).not.toHaveBeenCalled();
  });
});
