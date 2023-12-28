import type {Sort} from './';

export interface Pageable {
    readonly paged: boolean;
    readonly unpaged: boolean;
    readonly pageNumber: number;
    readonly pageSize: number;
    readonly offset: number;
    readonly sort: Sort;
}
