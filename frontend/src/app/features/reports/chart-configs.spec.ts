import { burndownChart, velocityChart } from './chart-configs';

describe('chart-configs', () => {
  it('trace le travail restant et la droite idéale, sans valeur pour les jours futurs', () => {
    const config = burndownChart([
      { date: '2026-11-16', remaining: 30, ideal: 30 },
      { date: '2026-11-17', remaining: 25, ideal: 15 },
      { date: '2026-11-18', ideal: 0 },
    ]);
    expect(config.type).toBe('line');
    expect(config.data.datasets[0].data).toEqual([30, 25, null]);
    expect(config.data.datasets[1].data).toEqual([30, 15, 0]);
    expect(config.data.labels?.length).toBe(3);
  });

  it('compare points engagés et terminés par sprint', () => {
    const config = velocityChart({
      sprints: [{ sprintId: 1, sprintName: 'Sprint 1', committed: 20, completed: 18 }],
      averageCompleted: 18,
    });
    expect(config.data.labels).toEqual(['Sprint 1']);
    expect(config.data.datasets.map((d) => d.data)).toEqual([[20], [18]]);
  });
});
