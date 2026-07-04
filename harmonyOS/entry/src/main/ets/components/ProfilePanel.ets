@Component
export struct ProfilePanel {
  @Prop hp: number = 0;
  @Prop maxHp: number = 0;
  @Prop attack: number = 0;
  @Prop defense: number = 0;
  @Prop exp: number = 0;
  @Prop coins: number = 0;

  build() {
    Row() {
      Text('DEF ' + this.defense.toString())
        .fontSize(13)
        .fontColor('#FFFFFF')
        .backgroundColor('#607D8B')
        .borderRadius(6)
        .padding({ left: 8, right: 8, top: 4, bottom: 4 })

      Text('ATK ' + this.attack.toString())
        .fontSize(13)
        .fontColor('#FFFFFF')
        .backgroundColor('#F44336')
        .borderRadius(6)
        .padding({ left: 8, right: 8, top: 4, bottom: 4 })

      Text('HP ' + this.hp.toString() + '/' + this.maxHp.toString())
        .fontSize(13)
        .fontColor('#FFFFFF')
        .backgroundColor('#4CAF50')
        .borderRadius(6)
        .padding({ left: 8, right: 8, top: 4, bottom: 4 })

      Text('EXP ' + this.exp.toString())
        .fontSize(13)
        .fontColor('#FFFFFF')
        .backgroundColor('#2196F3')
        .borderRadius(6)
        .padding({ left: 8, right: 8, top: 4, bottom: 4 })

      Text('Coin ' + this.coins.toString())
        .fontSize(13)
        .fontColor('#FFFFFF')
        .backgroundColor('#FF9800')
        .borderRadius(6)
        .padding({ left: 8, right: 8, top: 4, bottom: 4 })
    }
    .width('100%')
    .justifyContent(FlexAlign.SpaceAround)
    .padding({ top: 8, bottom: 8 })
    .backgroundColor('#1A1A2E')
  }
}
