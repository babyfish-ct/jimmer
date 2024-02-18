import type {Gender} from '../enums/';

export type BookStoreDto = {
    /**
     * Default BookStore DTO that contains all scalar properties
     */
    'BookStoreService/DEFAULT_FETCHER': {
        readonly id: number;
        readonly createdTime: string;
        readonly modifiedTime: string;
        readonly name: string;
        readonly website?: string | undefined;
    }
    /**
     * Simple BookStore DTO that only contains `id` and `name`
     */
    'BookStoreService/SIMPLE_FETCHER': {
        readonly id: number;
        readonly name: string;
    }
    /**
     * BookStore DTO contains
     * <ul>
     *     <li>all scalar properties</li>
     *     <li>The calculated-property `avgPrice`</li>
     *     <li>
     *         Associated `Book` objects provided by many-to-many property `books`,
     *         each `Book` object contains deeper `Author` objects.
     *     </li>
     * </ul>
     */
    'BookStoreService/WITH_ALL_BOOKS_FETCHER': {
        readonly id: number;
        readonly createdTime: string;
        readonly modifiedTime: string;
        readonly name: string;
        readonly website?: string | undefined;
        readonly avgPrice: number;
        readonly books: ReadonlyArray<{
            readonly id: number;
            readonly createdTime: string;
            readonly modifiedTime: string;
            readonly name: string;
            readonly edition: number;
            readonly price: number;
            readonly authors: ReadonlyArray<{
                readonly id: number;
                readonly createdTime: string;
                readonly modifiedTime: string;
                readonly firstName: string;
                readonly lastName: string;
                readonly gender: Gender;
            }>;
        }>;
    }
    /**
     * BookStore DTO contains
     * <ul>
     *     <li>all scalar properties</li>
     *     <li>The calculated-property `avgPrice`</li>
     *     <li>
     *         Associated `Book` objects provided by calculated association property `newestBooks`,
     *         each `Book` object contains deeper `Author` objects.
     *     </li>
     * </ul>
     */
    'BookStoreService/WITH_NEWEST_BOOKS_FETCHER': {
        readonly id: number;
        readonly createdTime: string;
        readonly modifiedTime: string;
        readonly name: string;
        readonly website?: string | undefined;
        readonly avgPrice: number;
        readonly newestBooks: ReadonlyArray<{
            readonly id: number;
            readonly createdTime: string;
            readonly modifiedTime: string;
            readonly name: string;
            readonly edition: number;
            readonly price: number;
            readonly authors: ReadonlyArray<{
                readonly id: number;
                readonly createdTime: string;
                readonly modifiedTime: string;
                readonly firstName: string;
                readonly lastName: string;
                readonly gender: Gender;
            }>;
        }>;
    }
}
