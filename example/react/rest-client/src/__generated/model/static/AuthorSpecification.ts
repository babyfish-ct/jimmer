import type {Gender} from '../enums/';

export interface AuthorSpecification {
    readonly firstName: string | null | undefined;
    readonly lastName: string | null | undefined;
    readonly gender: Gender | null | undefined;
    readonly minCreatedTime: string | null | undefined;
    readonly maxCreatedTime: string | null | undefined;
}
