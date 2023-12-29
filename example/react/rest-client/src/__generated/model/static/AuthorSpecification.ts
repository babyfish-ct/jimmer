import type {Gender} from '../enums/';

export interface AuthorSpecification {
    readonly firstName?: string | undefined;
    readonly lastName?: string | undefined;
    readonly gender?: Gender | undefined;
    readonly minCreatedTime?: string | undefined;
    readonly maxCreatedTime?: string | undefined;
}
