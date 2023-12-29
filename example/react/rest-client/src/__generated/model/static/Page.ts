import type {Pageable, Sort} from './';

export interface Page<T> {
    readonly totalPages: number;
    readonly totalElements: number;
    readonly number: number;
    readonly size: number;
    readonly numberOfElements: number;
    readonly content: ReadonlyArray<T>;
    readonly sort: Sort;
    readonly first: boolean;
    readonly last: boolean;
    readonly pageable: Pageable;
}
