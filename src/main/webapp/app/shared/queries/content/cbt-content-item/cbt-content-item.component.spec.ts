import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CbtContentItemComponent } from './cbt-content-item.component';

describe('CbtContentItemComponent', () => {
  let component: CbtContentItemComponent;
  let fixture: ComponentFixture<CbtContentItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CbtContentItemComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CbtContentItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
