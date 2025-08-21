import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-error-message',
  templateUrl: './error-message.component.html',
  styleUrls: ['./error-message.component.scss']
})
export class ErrorMessageComponent {
  @Input() message = 'An error occurred';
  @Input() type: 'error' | 'warning' | 'info' = 'error';
  @Input() showIcon = true;
  @Input() dismissible = false;
  
  dismissed = false;

  onDismiss(): void {
    this.dismissed = true;
  }

  get iconName(): string {
    const icons = {
      error: 'error',
      warning: 'warning', 
      info: 'info'
    };
    return icons[this.type];
  }
}