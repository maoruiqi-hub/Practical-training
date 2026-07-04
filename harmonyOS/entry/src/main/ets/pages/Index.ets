import { LoginPage } from './LoginPage';
import { TowerMapPage } from './TowerMapPage';
import { ShopPage } from './ShopPage';
import { AiTutorPage } from './AiTutorPage';
import { AbilityMapPage } from './AbilityMapPage';

@Entry
@Component
struct Index {
  @Provide navPathStack: NavPathStack = new NavPathStack();

  build() {
    Navigation(this.navPathStack) {
      LoginPage()
    }
    .mode(NavigationMode.Stack)
    .hideTitleBar(true)
    .navDestination(this.pageMap)
    .backgroundColor('#0D0D1A')
  }

  @Builder
  pageMap(name: string, param: Object) {
    if (name === 'TowerMap') {
      TowerMapPage()
    } else if (name === 'Shop') {
      ShopPage()
    } else if (name === 'AITutor') {
      AiTutorPage()
    } else if (name === 'AbilityMap') {
      AbilityMapPage()
    }
  }
}
