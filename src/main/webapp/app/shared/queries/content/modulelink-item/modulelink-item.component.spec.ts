import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModulelinkItemComponent } from './modulelink-item.component';

describe('ModulelinkItemComponent', () => {
  let component: ModulelinkItemComponent;
  let fixture: ComponentFixture<ModulelinkItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ModulelinkItemComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ModulelinkItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
