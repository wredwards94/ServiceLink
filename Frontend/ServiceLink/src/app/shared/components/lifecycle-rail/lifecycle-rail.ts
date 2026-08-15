import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
  ALLOWED_TRANSITIONS,
  LIFECYCLE_INDEX,
  LIFECYCLE_MAIN,
  STATUS_LABEL,
  Status,
} from '../../../models/ticket.model';

/** How a station on the rail is drawn. Luminance only — see styles.css. */
export type StationState = 'spent' | 'live' | 'open' | 'inert';

export interface Station {
  status: Status;
  label: string;
  state: StationState;
}

/**
 * The ticket lifecycle, drawn as the machine it actually is.
 *
 * TicketStatus.canTransitionTo is not a straight line — ON_HOLD is a siding off
 * IN_PROGRESS, and REOPENED is a return path from the two end states. Drawing
 * it as a linear stepper would misrepresent it, so the rail has three tracks.
 *
 * In `interactive` mode the legal targets for the current status are the only
 * things that can be pressed, which makes the control unable to compose an
 * illegal transition. The 400 handler upstream stays regardless: this map
 * mirrors the backend enum, and a mirror can drift.
 */
@Component({
  selector: 'app-lifecycle-rail',
  imports: [],
  templateUrl: './lifecycle-rail.html',
  styleUrl: './lifecycle-rail.css',
})
export class LifecycleRail {
  @Input({ required: true }) status!: Status;

  /** Renders the legal moves as buttons. Staff only — a USER gets 403 on PATCH. */
  @Input() interactive = false;

  /** The move currently in flight, so the pressed station can show it. */
  @Input() pending: Status | null = null;

  /** Row-sized variant: the four main stations as pips, no labels, no siding. */
  @Input() compact = false;

  @Output() move = new EventEmitter<Status>();

  readonly onHold = Status.ON_HOLD;
  readonly reopened = Status.REOPENED;
  readonly label = STATUS_LABEL;

  get mainStations(): Station[] {
    return LIFECYCLE_MAIN.map((status) => ({
      status,
      label: STATUS_LABEL[status],
      state: this.stateOf(status),
    }));
  }

  get holdStation(): Station {
    return {
      status: Status.ON_HOLD,
      label: STATUS_LABEL[Status.ON_HOLD],
      state: this.stateOf(Status.ON_HOLD),
    };
  }

  get reopenStation(): Station {
    return {
      status: Status.REOPENED,
      label: STATUS_LABEL[Status.REOPENED],
      state: this.stateOf(Status.REOPENED),
    };
  }

  /** True once the ticket has been through the reopen loop at least once. */
  get isReturned(): boolean {
    return this.status === Status.REOPENED;
  }

  private stateOf(station: Status): StationState {
    if (station === this.status) return 'live';

    const legal = ALLOWED_TRANSITIONS[this.status] ?? [];
    if (legal.includes(station)) return 'open';

    // Everything left of the current position on the main line has been passed
    // through. Off-main states are never "spent" — a ticket that was parked on
    // hold and resumed is not sitting on the siding any more, it is behind it.
    const here = LIFECYCLE_INDEX[this.status];
    const there = LIFECYCLE_INDEX[station];
    const onMain = LIFECYCLE_MAIN.includes(station);

    return onMain && there < here ? 'spent' : 'inert';
  }

  onStation(station: Station): void {
    if (!this.interactive || station.state !== 'open' || this.pending) return;
    this.move.emit(station.status);
  }

  /** Describes the whole control to a screen reader in one sentence. */
  get railDescription(): string {
    const legal = ALLOWED_TRANSITIONS[this.status] ?? [];
    const current = `Status ${STATUS_LABEL[this.status]}.`;
    if (!this.interactive) return current;
    if (legal.length === 0) return `${current} No further moves available.`;
    return `${current} Can move to ${legal.map((s) => STATUS_LABEL[s]).join(' or ')}.`;
  }
}
