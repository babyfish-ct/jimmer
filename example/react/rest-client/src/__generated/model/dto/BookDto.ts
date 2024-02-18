import type {Gender} from '../enums/';

export type BookDto = {
    /**
     * Complex Book DTO that contains
     * <ul>
     *     <li>All scalar properties except `tenant` of current `Book` entity</li>
     *     <li>`id`, `name` and the calculation property `avgPrice` of the associated `BookStore` object provided by many-to-one association `store`</li>
     *     <li>all scalar properties of the associated `Author` objects provided by many-to-many association `authors`</li>
     * </ul>
     */
    'BookService/COMPLEX_FETCHER': {
        readonly id: number;
        readonly createdTime: string;
        readonly modifiedTime: string;
        readonly name: string;
        readonly edition: number;
        readonly price: number;
        readonly store?: {
            readonly id: number;
            readonly createdTime: string;
            readonly modifiedTime: string;
            readonly name: string;
            readonly website?: string | undefined;
            readonly avgPrice: number;
        } | null | undefined;
        readonly authors: ReadonlyArray<{
            readonly id: number;
            readonly createdTime: string;
            readonly modifiedTime: string;
            readonly firstName: string;
            readonly lastName: string;
            readonly gender: Gender;
        }>;
    }
    /**
     * Default Book DTO that contains
     * <ul>
     *     <li>All scalar properties except `tenant` of current `Book` entity</li>
     *     <li>`id` and `name` of the associated `BookStore` object provided by many-to-one association `store`</li>
     *     <li>`id`, `firstName` and `lastName` of the associated `Author` objects provided by many-to-many association `authors`</li>
     * </ul>
     */
    'BookService/DEFAULT_FETCHER': {
        readonly id: number;
        readonly createdTime: string;
        readonly modifiedTime: string;
        readonly name: string;
        readonly edition: number;
        readonly price: number;
        readonly store?: {
            readonly id: number;
            readonly name: string;
        } | null | undefined;
        readonly authors: ReadonlyArray<{
            readonly id: number;
            readonly firstName: string;
            readonly lastName: string;
        }>;
    }
    /**
     * Simple Book DTO that only contains `id` and `name`
     */
    'BookService/SIMPLE_FETCHER': {
        readonly id: number;
        readonly name: string;
        readonly edition: number;
    }
}
