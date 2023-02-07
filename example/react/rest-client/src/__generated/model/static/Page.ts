import type { Pageable, Sort } from './';

export interface Page<T> {
    
    readonly content: ReadonlyArray<T>;
    
    readonly empty: boolean;
    
    readonly first: boolean;
    
    readonly last: boolean;
    
    readonly number: number;
    
    readonly numberOfElements: number;
    
    readonly pageable: Pageable;
    
    readonly size: number;
    
    readonly sort: Sort;
    
    readonly totalElements: number;
    
    readonly totalPages: number;
}
