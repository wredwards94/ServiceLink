import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LifecycleRail } from './lifecycle-rail';
import { ALLOWED_TRANSITIONS, Status } from '../../../models/ticket.model';

describe('LifecycleRail', () => {
  let fixture: ComponentFixture<LifecycleRail>;
  let component: LifecycleRail;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [LifecycleRail] }).compileComponents();
    fixture = TestBed.createComponent(LifecycleRail);
    component = fixture.componentInstance;
  });

  /**
   * The client map mirrors TicketStatus.canTransitionTo. If the backend enum
   * gains a move and this does not, the rail silently stops offering it — so
   * the shape is pinned here rather than trusted.
   */
  it('mirrors the backend transition table exactly', () => {
    expect(ALLOWED_TRANSITIONS).toEqual({
      [Status.NEW]: [Status.IN_PROGRESS],
      [Status.IN_PROGRESS]: [Status.ON_HOLD, Status.RESOLVED],
      [Status.ON_HOLD]: [Status.IN_PROGRESS],
      [Status.RESOLVED]: [Status.CLOSED, Status.REOPENED],
      [Status.REOPENED]: [Status.IN_PROGRESS],
      [Status.CLOSED]: [Status.REOPENED],
    });
  });

  it('marks the current station live and passed stations spent', () => {
    component.status = Status.RESOLVED;
    fixture.detectChanges();

    const byStatus = new Map(component.mainStations.map((s) => [s.status, s.state]));
    expect(byStatus.get(Status.NEW)).toBe('spent');
    expect(byStatus.get(Status.IN_PROGRESS)).toBe('spent');
    expect(byStatus.get(Status.RESOLVED)).toBe('live');
    // CLOSED is a legal move from RESOLVED, so it reads as open, not inert.
    expect(byStatus.get(Status.CLOSED)).toBe('open');
  });

  it('renders a button only for legal moves, and only when interactive', () => {
    component.status = Status.NEW;
    component.interactive = true;
    fixture.detectChanges();

    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('button.station'),
    );
    expect(buttons.length).toBe(1);
    expect(buttons[0].textContent).toContain('In progress');
  });

  it('renders no buttons for a user who cannot move the ticket', () => {
    component.status = Status.NEW;
    component.interactive = false;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('button.station').length).toBe(0);
  });

  it('emits the target when a legal station is pressed', () => {
    component.status = Status.RESOLVED;
    component.interactive = true;
    fixture.detectChanges();

    const moves: Status[] = [];
    component.move.subscribe((s) => moves.push(s));

    const buttons: HTMLButtonElement[] = Array.from(
      fixture.nativeElement.querySelectorAll('button.station'),
    );
    buttons.forEach((b) => b.click());

    expect(moves).toEqual([Status.CLOSED, Status.REOPENED]);
  });

  it('will not emit while a move is already in flight', () => {
    component.status = Status.NEW;
    component.interactive = true;
    component.pending = Status.IN_PROGRESS;
    fixture.detectChanges();

    const moves: Status[] = [];
    component.move.subscribe((s) => moves.push(s));
    component.onStation({ status: Status.IN_PROGRESS, label: 'In progress', state: 'open' });

    expect(moves).toEqual([]);
  });

  it('draws four pips in compact mode', () => {
    component.status = Status.IN_PROGRESS;
    component.compact = true;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.pip').length).toBe(4);
  });
});
