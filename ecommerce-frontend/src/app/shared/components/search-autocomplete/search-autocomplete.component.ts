import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Subject, takeUntil, map } from 'rxjs';
import { SearchService } from '@core/services/search.service';
import { SearchSuggestion } from '@shared/models/search.model';

@Component({
  selector: 'app-search-autocomplete',
  templateUrl: './search-autocomplete.component.html',
  styleUrls: ['./search-autocomplete.component.scss']
})
export class SearchAutocompleteComponent implements OnInit, OnDestroy {
  @Input() placeholder: string = 'Search products...';
  @Input() initialValue: string = '';
  @Output() search = new EventEmitter<string>();
  @Output() suggestionSelected = new EventEmitter<SearchSuggestion>();
  @ViewChild('searchInput', { static: true }) searchInput!: ElementRef<HTMLInputElement>;

  private destroy$ = new Subject<void>();
  private isProgrammaticChange = false;
  
  searchControl = new FormControl('');
  suggestions: SearchSuggestion[] = [];
  showSuggestions = false;
  isLoading = false;
  selectedIndex = -1;

  constructor(private searchService: SearchService) {}

  ngOnInit(): void {
    // Set initial value
    if (this.initialValue) {
      this.searchControl.setValue(this.initialValue);
    }

    // Setup real-time suggestions
    this.searchService.getSearchSuggestionsDebounced(
      this.searchControl.valueChanges!.pipe(
        map(value => value || '')
      )
    )
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (suggestions) => {
          this.suggestions = suggestions;
          // Only show suggestions if not programmatically set
          this.showSuggestions = !this.isProgrammaticChange && suggestions.length > 0;
          this.isLoading = false;
          this.selectedIndex = -1;
        },
        error: (error) => {
          console.error('Failed to get suggestions:', error);
          this.isLoading = false;
          this.showSuggestions = false;
        }
      });

    // Show loading state on input changes
    this.searchControl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(query => {
        if (query && query.length >= 2 && !this.isProgrammaticChange) {
          this.isLoading = true;
        } else {
          this.isLoading = false;
          if (!this.isProgrammaticChange) {
            this.showSuggestions = false;
          }
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onFocus(): void {
    const query = this.searchControl.value;
    if (query && query.length >= 2) {
      this.showSuggestions = this.suggestions.length > 0;
    }
  }

  onBlur(): void {
    // Delay hiding suggestions to allow clicks
    setTimeout(() => {
      this.showSuggestions = false;
      this.selectedIndex = -1;
    }, 200);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (!this.showSuggestions || this.suggestions.length === 0) {
      if (event.key === 'Enter') {
        this.performSearch();
      }
      return;
    }

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.selectedIndex = Math.min(this.selectedIndex + 1, this.suggestions.length - 1);
        break;

      case 'ArrowUp':
        event.preventDefault();
        this.selectedIndex = Math.max(this.selectedIndex - 1, -1);
        break;

      case 'Enter':
        event.preventDefault();
        if (this.selectedIndex >= 0 && this.selectedIndex < this.suggestions.length) {
          this.selectSuggestion(this.suggestions[this.selectedIndex]);
        } else {
          this.performSearch();
        }
        break;

      case 'Escape':
        event.preventDefault();
        this.showSuggestions = false;
        this.selectedIndex = -1;
        this.searchInput.nativeElement.blur();
        break;
    }
  }

  selectSuggestion(suggestion: SearchSuggestion): void {
    // Set flag to prevent showing suggestions when programmatically setting value
    this.isProgrammaticChange = true;
    this.showSuggestions = false;
    this.selectedIndex = -1;
    
    this.searchControl.setValue(suggestion.text);
    this.suggestionSelected.emit(suggestion);
    
    // Reset flag after a short delay to allow for debounced API call
    setTimeout(() => {
      this.isProgrammaticChange = false;
    }, 500);
    
    // Also trigger search
    this.performSearch();
  }

  performSearch(): void {
    const query = this.searchControl.value;
    if (query && query.trim()) {
      this.search.emit(query.trim());
      this.showSuggestions = false;
      this.selectedIndex = -1;
    }
  }

  getSuggestionIcon(type: string): string {
    switch (type) {
      case 'BRAND':
        return 'business';
      case 'CATEGORY':
        return 'category';
      case 'PRODUCT':
      default:
        return 'inventory_2';
    }
  }

  getSuggestionTypeLabel(type: string): string {
    switch (type) {
      case 'BRAND':
        return 'Brand';
      case 'CATEGORY':
        return 'Category';
      case 'PRODUCT':
      default:
        return 'Product';
    }
  }

  clearSearch(): void {
    this.searchControl.setValue('');
    this.showSuggestions = false;
    this.selectedIndex = -1;
    this.searchInput.nativeElement.focus();
  }
}