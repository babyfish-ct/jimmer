import { api } from "../common/ApiInstance";
import { MuxRow } from "../common/MuxRow";
import { ElementOf, ResponseOf } from "../__generated";

export type SimpleBookStore = MuxRow<ElementOf<ResponseOf<typeof api.bookStoreService.findSimpleStores>>>