import type {Dynamic_Author, Dynamic_BookStore} from './';

export interface Dynamic_Book {
    readonly id?: number;
    readonly name?: string;
    readonly edition?: number;
    readonly price?: number;
    readonly store?: Dynamic_BookStore | undefined;
    readonly authors?: ReadonlyArray<Dynamic_Author>;
    readonly storeId?: number | undefined;
    readonly authorIds?: ReadonlyArray<number>;
}
