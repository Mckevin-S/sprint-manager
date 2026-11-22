import { ChartConfiguration } from 'chart.js';
import { BurndownPoint, Velocity } from '../../core/models';

/** Couleurs lues dans les variables CSS, pour suivre le thème clair ou sombre. */
function cssVar(name: string, fallback: string): string {
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return value || fallback;
}

function axisColors() {
  return { grid: cssVar('--border', '#e6e3dc'), text: cssVar('--muted', '#6b6f80') };
}

const dayFormat = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' });

export function burndownChart(points: BurndownPoint[]): ChartConfiguration {
  const { grid, text } = axisColors();
  const accent = cssVar('--accent', '#e8590c');
  return {
    type: 'line',
    data: {
      labels: points.map((p) => dayFormat.format(new Date(p.date + 'T12:00:00'))),
      datasets: [
        {
          label: 'Travail restant',
          data: points.map((p) => p.remaining ?? null),
          borderColor: accent,
          backgroundColor: accent + '22',
          fill: true,
          tension: 0.25,
          pointRadius: 3,
          pointBackgroundColor: accent,
          stepped: false,
        },
        {
          label: 'Idéal',
          data: points.map((p) => p.ideal),
          borderColor: text,
          borderDash: [6, 6],
          borderWidth: 1.5,
          pointRadius: 0,
          fill: false,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { labels: { color: text, usePointStyle: true, boxWidth: 8 } },
        tooltip: { callbacks: { label: (c) => `${c.dataset.label} : ${c.parsed.y ?? '–'} pts` } },
      },
      scales: {
        x: { grid: { display: false }, ticks: { color: text, maxRotation: 0, autoSkip: true } },
        y: { beginAtZero: true, grid: { color: grid }, ticks: { color: text, precision: 0 }, title: { display: true, text: 'Points', color: text } },
      },
    },
  };
}

export function velocityChart(velocity: Velocity): ChartConfiguration {
  const { grid, text } = axisColors();
  const accent = cssVar('--accent', '#e8590c');
  return {
    type: 'bar',
    data: {
      labels: velocity.sprints.map((s) => s.sprintName),
      datasets: [
        { label: 'Engagés', data: velocity.sprints.map((s) => s.committed), backgroundColor: grid, borderRadius: 6, maxBarThickness: 36 },
        { label: 'Terminés', data: velocity.sprints.map((s) => s.completed), backgroundColor: accent, borderRadius: 6, maxBarThickness: 36 },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { labels: { color: text, usePointStyle: true, boxWidth: 8 } },
        tooltip: { callbacks: { label: (c) => `${c.dataset.label} : ${c.parsed.y} pts` } },
      },
      scales: {
        x: { grid: { display: false }, ticks: { color: text } },
        y: { beginAtZero: true, grid: { color: grid }, ticks: { color: text, precision: 0 } },
      },
    },
  };
}
