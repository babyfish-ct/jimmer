import type {Gender} from '../enums/';

export interface CompositeBookInput_TargetOf_authors {
    readonly firstName: string;
    readonly lastName: string;
    readonly gender: Gender;
}
