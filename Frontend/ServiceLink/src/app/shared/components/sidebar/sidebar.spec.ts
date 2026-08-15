import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { Sidebar } from './sidebar';

describe('Sidebar', () => {
  let component: Sidebar;
  let fixture: ComponentFixture<Sidebar>;
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [Sidebar],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Sidebar);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('does not look up a profile when nobody is signed in', () => {
    fixture.detectChanges();
    http.expectNone(() => true);
  });

  it('shows the administration section only to an admin', () => {
    localStorage.setItem('role', 'AGENT');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('People');

    const adminFixture = TestBed.createComponent(Sidebar);
    localStorage.setItem('role', 'ADMIN');
    adminFixture.detectChanges();
    expect(adminFixture.nativeElement.textContent).toContain('People');
  });
});
