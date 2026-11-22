import { RelativeTimePipe } from './relative-time.pipe';

describe('RelativeTimePipe', () => {
  const pipe = new RelativeTimePipe();
  const now = new Date('2026-11-20T12:00:00Z');

  it('formate les durées en français', () => {
    expect(pipe.transform('2026-11-20T11:59:30Z', now)).toBe("à l'instant");
    expect(pipe.transform('2026-11-20T09:00:00Z', now)).toBe('il y a 3 heures');
    expect(pipe.transform('2026-11-19T12:00:00Z', now)).toBe('hier');
    expect(pipe.transform(null, now)).toBe('');
  });
});
