import type { Sort } from './';

export interface Pageable {
    
    readonly offset: number;
    
    readonly pageNumber: number;
    
    readonly pageSize: number;
    
    readonly paged: boolean;
    
    readonly sort: Sort;
    
    readonly unpaged: boolean;
}
