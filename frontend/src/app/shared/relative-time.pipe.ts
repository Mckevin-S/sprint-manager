import { Pipe, PipeTransform } from '@angular/core';

const UNITS: [Intl.RelativeTimeFormatUnit, number][] = [
  ['year', 31_536_000],
  ['month', 2_592_000],
  ['week', 604_800],
  ['day', 86_400],
  ['hour', 3_600],
  ['minute', 60],
];

/** « il y a 3 heures », « hier »... */
@Pipe({ name: 'relativeTime', standalone: true })
export class RelativeTimePipe implements PipeTransform {
  private readonly format = new Intl.RelativeTimeFormat('fr', { numeric: 'auto' });

  transform(value: string | Date | null | undefined, now: Date = new Date()): string {
    if (!value) {
      return '';
    }
    const seconds = Math.round((new Date(value).getTime() - now.getTime()) / 1000);
    for (const [unit, size] of UNITS) {
      if (Math.abs(seconds) >= size) {
        return this.format.format(Math.round(seconds / size), unit);
      }
    }
    return "à l'instant";
  }
}
